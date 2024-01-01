package org.cis1200;

public class AdvancedManipulations {

    /**
     * Change the contrast of a picture.
     *
     * Your job is to change the intensity of the colors in the picture.
     * The simplest method of changing contrast is as follows:
     *
     * 1. Find the average color intensity of the picture.
     * a) Sum the values of all the color components for each pixel.
     * b) Divide the total by the number of pixels times the number of
     * components (3).
     * 2. Subtract the average color intensity from each color component of
     * each pixel. This will make the average color intensity zero.
     * Note that you could underflow into negatives. This is fine.
     * 3. Scale the intensity of each pixel's color components by multiplying
     * them by the "multiplier" parameter. Note that the multiplier is a
     * double (a decimal value like 1.2 or 0.6) and color values are ints
     * between 0 and 255.
     * 4. Add the original average color intensity back to each component of
     * each pixel.
     * 5. Clip the color values so that all color component values are between
     * 0 and 255. (This should be handled by the Pixel class anyway!)
     *
     * Hint: You should use Math.round() before casting to an int for
     * the average color intensity and for the scaled RGB values.
     * (I.e., in particular, the average should be rounded to an int
     * before being used for further calculations...)
     *
     * @param pic        the original picture
     * @param multiplier the factor by which each color component
     *                   of each pixel should be scaled
     * @return the new adjusted picture
     * 
     */
    public static PixelPicture adjustContrast(PixelPicture pic, double multiplier) {
        int h = pic.getHeight();
        int w = pic.getWidth();
        Pixel[][] src = pic.getBitmap();

        // Finding avg color intensity
        int sum = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                Pixel p = src[i][j];
                sum += p.getRed();
                sum += p.getGreen();
                sum += p.getBlue();
            }
        }
        int avgIntensity = (int) Math.round(sum / (double) (3 * h * w));

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                Pixel pOld = src[i][j];
                int rOld = pOld.getRed();
                int gOld = pOld.getGreen();
                int bOld = pOld.getBlue();


                int rScaled = (int) Math.round(multiplier * (rOld - avgIntensity));
                int gScaled = (int) Math.round(multiplier * (gOld - avgIntensity));
                int bScaled = (int) Math.round(multiplier * (bOld - avgIntensity));

                int rNew = avgIntensity + rScaled;
                int gNew = avgIntensity + gScaled;
                int bNew = avgIntensity + bScaled;

                src[i][j] = new Pixel(rNew, gNew, bNew);
            }
        }

        return new PixelPicture(src);
    }

    /**
     * Reduce a picture to its most common colors.
     *
     * You will need to make use of the ColorMap class to generate a map from
     * Pixels of a certain color to the frequency with which pixels of that
     * color appear in the image. If you go to the ColorMap class, you will
     * notice that it does not have an explicitly declared constructor. In
     * those cases, Java provides a default constructor, which you can call
     * with no arguments as follows:
     * 
     * ColorMap m = new ColorMap();
     * 
     * You will then go on to populate your ColorMap by adding pixels and their
     * corresponding frequencies.
     * 
     * Once you have generated your ColorMap, select your palette by
     * retrieving the first 'numColors' (see parameter description below)
     * pixels with colors that appear with the highest frequency. Then
     * change each pixel in the picture to one with the closest matching
     * color from your palette.
     *
     * Note that if there are two different colors that are the *same* minimal
     * distance from the given color, your code should select the most
     * frequently appearing one as the new color for the pixel. If both colors
     * appear with the same frequency, your code should select the one that
     * appears *first* in the output of the ColorMap's getSortedPixels.
     *
     * Algorithms like this are widely used in image compression. GIFs in
     * particular compress the palette to no more than 255 colors. The variant
     * we have implemented here is a weak one, since it only counts color
     * frequency by exact match. Advanced palette reduction algorithms (known as
     * "indexing" algorithms) calculate color regions and distribute the palette
     * over the regions. For example, if our picture had a lot of shades of blue
     * and little red, our algorithm would likely choose a palette of
     * all blue colors. An advanced algorithm would recognize that blues look
     * similar and distribute the palette so that it would be possible to
     * display red as well.
     *
     * @param pic       the original picture
     * @param numColors the maximum number of colors that can be used in the
     *                  reduced picture
     * @return the new reduced picture
     */
    public static PixelPicture reducePalette(PixelPicture pic, int numColors) {
        ColorMap cm = new ColorMap();

        // Populating the map
        Pixel[][] src = pic.getBitmap();
        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[i].length; j++) {
                Pixel p = src[i][j];
                if (cm.contains(p)) {
                    cm.put(p, cm.getValue(p) + 1);
                } else {
                    cm.put(p, 1);
                }
            }
        }

        Pixel[] mostFrequent = cm.getSortedPixels();

        for (int i = 0; i < src.length; i++) {
            for (int j = 0; j < src[i].length; j++) {
                Pixel p = src[i][j];
                int minDistance = Integer.MAX_VALUE;
                Pixel closest = null;
                // we must iterate from the "end"
                int endIndex = Math.min(numColors, mostFrequent.length);
                for (int k = endIndex - 1; k >= 0; k--) {
                    if (closest == null || p.distance(mostFrequent[k]) < minDistance) {
                        closest = mostFrequent[k];
                        minDistance = p.distance(mostFrequent[k]);
                        // time complexity: O(n^2 * numColors) (rip lol)
                    }
                }
                src[i][j] = closest;
            }
        }
        return new PixelPicture(src);
    }

    /**
     * This method blurs an image.
     *
     * PLEASE read about the *required* division implementation below - even
     * if you understand the rest of the implementation, slight floating-point
     * errors can cause significant autograder deductions!
     *
     * The general idea is that to determine the color of a pixel at
     * coordinate (x, y) of the result, look at (x, y) in the input image
     * as well as the pixels within a box (details below) centered at (x, y).
     * The average color of the pixels in the box - determined by separately
     * averaging R, G, and B - will be the color of (x, y) in the result.
     *
     * How big is the box? That's defined by {@code radius}. A radius of 1
     * yields a 3x3 box (all pixels 1 step away, including diagonals).
     * Similarly, a radius of 2 yields a 5x5 box, a radius of 3 a 7x7 box, etc.
     *
     * As an example, say we have the following image - each pixel is written
     * as (r, g, b) - and the radius parameter is 1.
     *
     * ( 1, 13, 25) ( 2, 14, 26) ( 3, 15, 27) ( 4, 16, 28)
     * ( 5, 17, 29) ( 6, 18, 30) ( 7, 19, 31) ( 8, 20, 32)
     * ( 9, 21, 33) (10, 22, 34) (11, 23, 35) (12, 24, 36)
     *
     * If we wanted the color of the output pixel at (1, 1), we would look at
     * the radius-1 box surrounding (1, 1) in the original image, which is
     *
     * ( 1, 13, 25) ( 2, 14, 26) ( 3, 15, 27)
     * ( 5, 17, 29) ( 6, 18, 30) ( 7, 19, 31)
     * ( 9, 21, 33) (10, 22, 34) (11, 23, 35)
     *
     * The average red component is
     * (1 + 2 + 3 + 5 + 6 + 7 + 9 + 10 + 11) / 9 = 6, so the result
     * pixel at (1, 1) should have red component 6.
     *
     * If the target pixel is on the edge, you should average the pixels
     * within the radius that exist. So in the same example above, the color of
     * the output at (0, 0) would be the average of:
     *
     * ( 1, 13, 25) ( 2, 14, 26)
     * ( 5, 17, 29) ( 6, 18, 30)
     *
     * **IMPORTANT FLOATING POINT NOTE:** To compute the average in a way that's
     * compatible with our autograder, please do the following steps in order:
     *
     * 1. Use floating-point division (not integer division) to divide the
     * total red/green/blue amounts by the number of pixels.
     * 2. Use Math.round() on the result of 1. This is still a float, but it
     * has been rounded to the nearest integer value.
     * 3. Cast the result of 2 to an int. That should be the component's value
     * in the output picture.
     *
     * @param pic    The picture to be blurred.
     * @param radius The radius of the blurring box.
     * @return A blurred version of the original picture.
     */
    public static PixelPicture blur(PixelPicture pic, int radius) {
        int h = pic.getHeight();
        int w = pic.getWidth();

        Pixel[][] src = pic.getBitmap();
        Pixel[][] res = new Pixel[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                // find average color values within a certain radius
                int rSum = 0;
                int gSum = 0;
                int bSum = 0;
                for (int row = i - radius;  row <= i + radius && row < h; row++) {
                    if (row >= 0) {
                        for (int col = j - radius; col <= j + radius && col < w; col++) {
                            if (col >= 0) {
                                Pixel p = src[row][col];
                                rSum += p.getRed();
                                gSum += p.getGreen();
                                bSum += p.getBlue();
                            }
                        }
                    }
                }
                // Note that the size of the area depends on the i, j, and radius
                int dUp = i - radius < 0 ? i : radius;
                int dDown = i + radius >= h ? h - i - 1 : radius;
                int dLeft = j - radius < 0 ? j : radius;
                int dRight = j + radius >= w ? w - j - 1 : radius;

                int area = (dUp + dDown + 1) * (dLeft + dRight + 1);
                int rAvg = (int) Math.round(rSum / (float) area);
                int gAvg = (int) Math.round(gSum / (float) area);
                int bAvg = (int) Math.round(bSum / (float) area);

                res[i][j] = new Pixel(rAvg, gAvg, bAvg);
            }
        }
        return new PixelPicture(res);
    }

    // NOTE: You may want to add a static helper function here to
    // help find the average color around the pixel you are blurring.

    /**
     * Challenge Problem (this problem is worth 0 points):
     * Flood pixels of the same color with a different color.
     *
     * The name is short for flood fill, which is the familiar "paint bucket"
     * operation in graphics programs. In a paint program, the user clicks on a
     * point in the image. Every neighboring, similarly-colored point is then
     * "flooded" with the color the user selected.
     *
     * Suppose we want to flood color at (x,y). The simplest way to do flood
     * fill is as follows:
     *
     * 1. Let target be the color at (x,y).
     * 2. Create a set of points Q containing just the point (x,y).
     * 3. Take the first point p out of Q.
     * 4. Set the color at p to color.
     * 5. For each of p's non-diagonal neighbors - up, down, left, and right -
     * check to see if they have the same color as target. If they do, add
     * them to Q.
     * 6. If Q is empty, stop. Otherwise, go to 3.
     *
     * This is a naive algorithm that can be made significantly faster if you
     * wish to try.
     *
     * For Q, you should use the provided IntQueue class. It works very much
     * like the queues we implemented in OCaml.
     *
     * @param pic The original picture to be flooded.
     * @param c   The pixel the user "clicked" (representing the color that should
     *            be flooded).
     * @param row The row of the point on which the user "clicked."
     * @param col The column of the point on which the user "clicked."
     * @return A new picture with the appropriate region flooded.
     */
    public static PixelPicture flood(PixelPicture pic, Pixel c, int row, int col) {
        return pic;
    }
}
