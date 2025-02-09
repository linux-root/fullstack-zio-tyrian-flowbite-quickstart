const path = require('path')
const scalaVersion = "3.6.2"

const scalaAppEntrypoint = (env) => {
  if (env === 'development') {
    return path.resolve(__dirname, `target/scala-${scalaVersion}/frontend-fastopt/main.js`);
  } else if (env === 'production') {
    return path.resolve(__dirname, `target/scala-${scalaVersion}/frontend-opt/main.js`);
  } else {
    console.error(`Loading output scala version ${scalaVersion}. Unknown env ${env}`)
  }
}
exports.scalaAppEntrypoint = scalaAppEntrypoint
