# Image Synthesizer
This plugin generates images from mathematical functions and primitive patterns in a predefined coordinate system. It can be used to synthesize test-cards to analyze for example the behavior of filter-operations or to test the functionality of various image-processing algorithms. The tool can also be used in a teaching environment, where example images for presentations can be prepared or even to demonstrate basic principles of digital image processing. The UI makes it possible to play around various parameters and will show a preview of the result. Further on, an artistic use to create images stacks or videos, is also imaginable.

## Modes
### Functions to image
The function value of a two- or three-dimensional function f(x, y) or f(x, y, z) is interpreted as the intensity value of the image.

![GUI screenshot functions](/screenshot_functions.jpg)

### Conditional to image
Generate primitive patterns using boolean expressions with ImageJ macro code. The conditional will be executed for each pixel in the image inside the given coordinate range.

![GUI screenshot conditional](/screenshot_conditional.jpg)

## Development
This Project started off the [minimal Maven project](https://github.com/imagej/minimal-ij1-plugin) implementing an ImageJ 1.x plugin.

## Authors

- [Maximilian Maske](mailto:m.maske@posteo.de).

## License

MIT. See [LICENSE.txt](/LICENSE.txt).
