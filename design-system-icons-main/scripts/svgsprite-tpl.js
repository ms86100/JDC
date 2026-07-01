/**
 * Utility script to generate SVG sprite html template
 */
var fs = require("fs");
var path = require("path");
const Handlebars = require("handlebars");

const config = {
  ...require("../fantasticonrc.js"),
  input: "dist/icons-flatten",
  dest: "dist/svgsprite/index.html",
};
const assets = {};

const parseFolder = (folderPath) => {
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    if (file.isDirectory()) {
      parseFolder(path.join(folderPath, file.name));
    } else if (file.name.indexOf(".svg") > 0) {
      assets[file.name.split(".")[0]] = {};
    }
  });
};

console.log(`[scripts/svgsprite-tpl.js] generate ${config.dest}`);

// Parse and add all svgs
parseFolder(config.input);

const templateFile = fs.readFileSync("templates/svgsprite-html.hbs", {
  encoding: "utf-8",
});
const template = Handlebars.compile(templateFile);

// output
console.log(`Found ${Object.keys(assets).length} icons`);
fs.writeFileSync(config.dest, template({
  ...config,
  assets,
  count: Object.keys(assets).length
}));
