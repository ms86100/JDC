/**
 * Utility script to generate SVG Sprite from a folder of svg icons
 */
var svgstore = require("svgstore");
var fs = require("fs");
var path = require("path");

var config = {
  input: "dist/icons-flatten",
  dest: "dist/svgsprite/airbus-icons.svg",
  // https://github.com/svgstore/svgstore#svgstore-options
  options: {},
};

// Create svgstore instance
var count = 0;
var sprites = svgstore(config.options);

const addSVG = (name, svgPath) => {
  const id = name.split(".")[0];
  count++
  sprites.add(id, fs.readFileSync(svgPath, { encoding: "utf-8" }));
};

const parseFolder = (folderPath) => {
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    if (file.isDirectory()) {
      parseFolder(path.join(folderPath, file.name));
    } else if (file.name.indexOf(".svg") > 0) {
      addSVG(file.name, path.join(folderPath, file.name));
    }
  });
};

console.log(`[scripts/svgsprite-build.js] build ${config.dest}`);

// Parse and add all svgs
parseFolder(config.input);

// output
console.log(`Found ${count} icons`);
const content = sprites.toString();
fs.writeFileSync(config.dest, content);
