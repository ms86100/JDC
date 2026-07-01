/**
 * Utility script to flatten svg folders before generate webfont
 */
const fs = require("fs-extra");
const path = require("path");

const flattenFolder = (folderPath, destPath, postfix) => {
  // console.log("flattenFolder", folderPath);
  fs.ensureDirSync(destPath);
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    const filePath = path.join(folderPath, file.name);
    if (file.isDirectory()) {
      flattenFolder(filePath, destPath, postfix);
    } else {
      if (file.name.indexOf(".svg") > 0) {
        if (postfix) {
          const split = file.name.split(".");
          file.name = split[0] + postfix + ".svg";
        }
        fs.copySync(filePath, path.join(destPath, file.name));
      }
    }
  });
};

console.log("[scripts/icons-flatten.js] flatten icons folder");

fs.removeSync("dist/icons-flatten");
flattenFolder("dist/icons", "dist/icons-flatten");
