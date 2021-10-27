const devExpr = /^http[s]?:\/\/.+?cef\.iohkdev\.io:\d+\/?$/;
const localExpr = /^http[s]?:\/\/localhost:\d+\/?$/;

export const isDevEnv = backendUrl => devExpr.test(backendUrl) || localExpr.test(backendUrl);
