// see https://react-svgr.com/docs/options/
module.exports = {
  // Replace SVG "width" and "height" value by "1em" in order to make SVG size inherits from text size.
  icon: true,
  // All properties given to component will be forwarded on SVG tag.
  expandProps: "end",
  // specify jsx runtime
  jsxRuntime: "automatic",
  // Generates .tsx files with TypeScript typings
  // typescript: false,
};
