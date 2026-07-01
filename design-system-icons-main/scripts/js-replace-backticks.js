/**
 * Utility script to replace backticks with single quotes in js modules exported from svg-to-ts.
 * Backticks are useless and it as compatibility issues with IE11/etc.
 */
const fs = require("fs-extra");
const path = require("path");

let count = 0;

const replaceBackticks = (folderPath) => {
  fs.ensureDirSync(folderPath);
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    const filePath = path.join(folderPath, file.name);
    if (file.isDirectory()) {
      flattenFolder(filePath);
    } else {
      if (file.name.indexOf(".js") > 0) {
        let fileContent = fs.readFileSync(filePath, { encoding: "utf-8" });
        if (fileContent.indexOf("`") >= 0) count++;
        fs.writeFileSync(filePath, fileContent.replace(/`/g, "'"), { encoding: "utf-8" });
      }
    }
  });
};

console.log("[scripts/js-replace-backticks.js] replace backticks with single quotes");

replaceBackticks("dist/js");

console.log("[scripts/js-replace-backticks.js] updated", count, "files");