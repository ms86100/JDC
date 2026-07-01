/**
 * Utility script to optimize svg exported from sketch
 * @see https://github.com/svg/svgo
 */
const fs = require("fs");
const path = require("path");
const SVGO = require("svgo");

console.log("[scripts/export-svgo.js] optimize svgs");

let config;
SVGO.loadConfig().then((result) => {
  console.log("CONFIG", JSON.stringify(result, null, 2));
  config = result;
  parseFolder("dist/icons");
});

const parseFolder = (folderPath) => {
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    if (file.isDirectory()) {
      parseFolder(path.join(folderPath, file.name));
    } else if (file.name.indexOf(".svg") > 0) {
      const svgPath = path.join(folderPath, file.name);
      const svgString = SVGO.optimize(fs.readFileSync(svgPath, "utf8"), { path: svgPath, ...config }); 
      fs.writeFileSync(svgPath, svgString.data);
    }
  });
};

