module.exports = {
  name: "airbus-icons",
  prefix: "ds-icon",
  // use a CSS selector instead of 'tag + prefix' (default: null)
  // selector: ".ds-icon",
  inputDir: "./dist/icons-flatten",
  outputDir: "./dist/webfont",
  fontTypes: ["ttf", "woff", "woff2"],
  assetTypes: ["ts", "css", "sass", "scss", "json", "html"],
  // public URL to the fonts directory (used in the generated CSS)
  // fontsUrl: '/static/fonts',
  formatOptions: {
    // Pass options directly to `svgicons2svgfont`
    woff: {
      // Woff Extended Metadata Block - see https://www.w3.org/TR/WOFF/#Metadata
      // metadata: '...'
    },
    json: {
      // render the JSON human readable with two spaces indentation (default is none, so minified)
      indent: 2,
    },
    ts: {
      // select what kind of types you want to generate (default `['enum', 'constant', 'literalId', 'literalKey']`)
      types: ["constant", "literalId"],
      // render the types with `'` instead of `"` (default is `"`)
      singleQuotes: true,
    },
  },
  // Use a custom Handlebars template
  templates: {
    html: "./templates/webfont-html.hbs",
  },
  pathOptions: {
    //   ts: './src/types/icon-types.ts',
    //   json: './misc/icon-codepoints.json'
  },
};
