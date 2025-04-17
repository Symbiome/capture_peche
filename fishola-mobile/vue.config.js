module.exports = {
  // options...
  outputDir: 'target/dist-' + process.env.DIST_FOLDER,
  // Adding this allows to debug in firefox/chrome
  configureWebpack: {
    devtool: 'source-map'
  }
}
const {gitDescribeSync} = require('git-describe');
// process.env.VITE__GIT_HASH = JSON.stringify(gitDescribeSync());
process.env.VITE__GIT_REVISION = gitDescribeSync().suffix;
process.env.VITE__VERSION = require('./package.json').version;
process.env.VITE__PROJECT_VERSION = process.env.MAVEN_PROJECT_VERSION || 'N/A';
