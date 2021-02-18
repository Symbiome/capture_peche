module.exports = {
  // options...
  outputDir: 'target/dist-' + process.env.DIST_FOLDER,
  // Adding this allows to debug in firefox/chrome
  configureWebpack: {
    devtool: 'source-map'
  }
}
const {gitDescribeSync} = require('git-describe');
// process.env.VUE_APP_GIT_HASH = JSON.stringify(gitDescribeSync());
process.env.VUE_APP_GIT_REVISION = gitDescribeSync().suffix;
process.env.VUE_APP_VERSION = require('./package.json').version;
process.env.VUE_APP_PROJECT_VERSION = process.env.MAVEN_PROJECT_VERSION || 'N/A';
