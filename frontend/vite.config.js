import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig(({ command }) => {
  return {
    resolve: {
      alias: {
        "scalajs": command === "serve" ? './target/scala-3.7.1/frontend-fastopt/main.js' : './target/scala-3.7.1/frontend-opt/main.js',
        "resources": path.resolve(__dirname, "./src/main/resources"),
        "js": path.resolve(__dirname, "./src/main/js"),
      }
    },
    server: {
      port: 9876,
      historyApiFallback: true
    },
  };
});
