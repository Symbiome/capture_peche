module.exports = {
  // options...
  outputDir: 'target/dist-' + process.env.DIST_FOLDER,
  publicPath: process.env.PUBLIC_PATH_FOLDER ?  process.env.PUBLIC_PATH_FOLDER : '/',
  // Adding this allows to debug in firefox/chrome
  configureWebpack: {
    devtool: 'source-map'
  }
}
