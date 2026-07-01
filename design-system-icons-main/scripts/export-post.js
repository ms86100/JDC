/**
 * Utility script for svg folders and file names
 * - trim whitespace
 * - lowercase
 * - fix names starting with numeric literals
 */
const fs = require("fs-extra");
const path = require("path");

let totalFiles = 0;
let totalDirectories = 0;
let changedFiles = 0;
let changedDirectories = 0;

// from https://www.thoughtco.com/how-to-convert-numbers-to-words-with-javascript-4072535
const numberToWords = (s) => {
  const dg = ['zero','one','two','three','four','five','six','seven','eight','nine'];
  const tn = ['ten','eleven','twelve','thirteen', 'fourteen','fifteen','sixteen', 'seventeen','eighteen','nineteen'];
  const tw = ['twenty','thirty','forty','fifty','sixty','seventy','eighty','ninety']; 
  
  s = s.toString();
  s = s.replace(/[\, ]/g,'');
  if (s != parseFloat(s)) return 'not a number';
  var x = s.length;
  var n = s.split('');
  var str = '';
  var sk = 0;
  for (var i=0; i < x; i++) {
    if ((x-i)%3 == 2) {
      if (n[i] == '1') {
        str += tn[Number(n[i+1])] + '-';
        i++;sk=1;
      } else if (n[i]!=0) {
        str += tw[n[i]-2] + '-';
        sk=1;
      }
    } else if (n[i]!=0) {
      str += dg[n[i]] + '-';
      if ((x-i)%3==0) str += 'hundred-';
      sk=1;
    }
  }
  str = str.replace(/-$/, "");
  return str;
}

const replaceStartingNums = (str) => {
  if (!str.match(/^\d/)) {
    return str;
  }
  const num = str.match(/\d+/)[0];
  const words = numberToWords(num); 
  console.log("replaceStartingNums > start with a number", str, num, words);
  return str.replace(num, words);
}

const removeEndSpaces = (str) => {
  if (str.indexOf(".") < 0) return str;
  const parts = str.split(".");
  return `${parts[0].trim()}.${parts[1]}`;
}

const cleanFile = (folderPath, fileName, isDirectory = false) => {
  isDirectory ? totalDirectories++ : totalFiles++;
  const cleanFileName = replaceStartingNums(removeEndSpaces(
    fileName.trim().toLowerCase()
  ));
  const result = path.join(folderPath, cleanFileName);
  if (fileName !== cleanFileName) {
    console.log(`cleanFile from '${fileName}' to '${cleanFileName}'`);
    isDirectory ? changedDirectories++ : changedFiles++;
    const src = path.join(folderPath, fileName);
    try {
      fs.renameSync(src, result);
    } catch (err) {
      throw new Error(`
      Unable to rename "${src}" to "${result}"
      Please check that you don't have different formating of this ${
        isDirectory ? "category" : "icon"
      } name in Sketch\n
      ${err}
      `);
    }
  }
  return result;
};

const parseFolder = (folderPath) => {
  console.log("parseFolder", folderPath);
  fs.readdirSync(folderPath, { withFileTypes: true }).forEach((file) => {
    if (file.isDirectory()) {
      parseFolder(cleanFile(folderPath, file.name, true));
    } else {
      cleanFile(folderPath, file.name);
    }
  });
};

console.log(
  "[scripts/export-post.js] cleanup exported files and folders names"
);
parseFolder("dist/icons");
console.log(`Directories updated: ${changedDirectories}/${totalDirectories}`);
console.log(`Files updated: ${changedFiles}/${totalFiles}`);
