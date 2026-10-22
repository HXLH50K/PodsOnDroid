const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const variant = process.env.APK_VARIANT;
if (!['release', 'debug'].includes(variant)) throw new Error('APK_VARIANT must be release or debug');

const directory = path.join('app/build/outputs/apk', variant);
const metadata = JSON.parse(fs.readFileSync(path.join(directory, 'output-metadata.json'), 'utf8'));
if (metadata.elements.length !== 1) throw new Error('Exactly one APK artifact is required');

const artifact = metadata.elements[0];
const source = path.join(directory, artifact.outputFile);
// The shortened digest identifies the artifact; verification uses the full hash.
const shortHash = crypto.createHash('sha256').update(fs.readFileSync(source)).digest('hex').slice(0, 12);
const suffix = variant === 'debug' ? '-debug' : '';
const filename = `PODS-${artifact.versionName}${suffix}-${shortHash}.apk`;
const destination = path.join(directory, filename);
fs.renameSync(source, destination);
fs.appendFileSync(process.env.GITHUB_OUTPUT, `path=${destination}\n`);
