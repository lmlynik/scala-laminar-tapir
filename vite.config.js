import { defineConfig } from "vite";
import path from "path";
import scalaJSPlugin from '@scala-js/vite-plugin-scalajs';

function isDev() {
    return process.env.NODE_ENV !== "production";
}

const root = path.resolve('frontend/src/main/resources')

const proxy = {
    "/": {
        target: "http://0.0.0.0:9000", // server ip to proxy api in dev mode
        bypass: function(req, res, proxyOptions) {
            // regex matching snippet ids
            const snippet = /(\/[A-Za-z0-9]{22}|\/[A-Za-z0-9]{22}\/([A-Za-z0-9])*[/(0-9)*])/;
            const snippetOld = /(\/[0-9]+)/;
            const backendUrls = /(\/api\/|\/login|\/logout|\/*.wasm|\/*.scm)/;

            if(!backendUrls.test(req.url)) {
                if (snippet.test(req.url) || snippetOld.test(req.url)) {
                    console.log("index: " + req.url);
                    return "/";
                } else {
                    if (req.url.startsWith("/try")) {
                        return "/";
                    } else {
                        console.log("other: " + req.url);
                        return req.url;
                    }
                }
            } else {
                console.log("proxied: " + req.url);
            }
        }
    }
}

export default defineConfig({
    define: {
        'process.env.NODE_ENV': `"${process.env.NODE_ENV}"`,
    },
    root: root,
    base: isDev() ? '' : '/public/',
    plugins: [
        scalaJSPlugin({
            projectID: 'frontend'
        }),
    ],
    resolve: {
        alias: [
            {
                find: '@resources',
                replacement: path.resolve(__dirname, 'frontend', 'src', 'main', 'resources'),
            },
        ],
    },
    build: {
        outDir: path.resolve(__dirname, 'frontend', 'dist', 'public'),
        rollupOptions: {
            input: {
                app: path.resolve(root, 'index.html')
            },
            output: {
                entryFileNames: "[name]-[hash].js",
                assetFileNames: "assets/[name]-[hash].[ext]"
            }
        },
    },
    css: {
        devSourcemap: true,
    },
    server: {
        proxy: proxy,
        port: 8080,
        strictPort: true,
    },
    preview: {
        port: 8080,
        strictPort: true,
    }
});