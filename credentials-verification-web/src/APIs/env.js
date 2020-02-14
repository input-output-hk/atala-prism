const devExpr = new RegExp('^http[s]?:\\/\\/.+?cef\\.iohkdev\\.io:\\d+\\/?$');
const localExpr = new RegExp('^http[s]?:\\/\\/localhost:\\d+\\/?$');

export const isDevEnv = backendUrl => {
  return devExpr.test(backendUrl) || localExpr.test(backendUrl);
};
