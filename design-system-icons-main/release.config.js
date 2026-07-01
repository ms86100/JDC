// @see https://semantic-release.gitbook.io/semantic-release/usage/configuration#configuration
module.exports = {
  branches: ["main", { name: "next", prerelease: "beta" }],
  plugins: [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    "@semantic-release/npm",
    // generate package zip file to upload to github release
    [
      "@semantic-release/exec",
      {
        publishCmd: "npm pack",
      },
    ],
    "@semantic-release/github",
  ],
  assets: [
    {
      path: "airbus-icons-*.tgz",
      name: "airbus-icons-${nextRelease.version}.tgz",
    },
  ],
};
