const RETRY_COUNT_DEFAULT = 5;
const RETRY_INTERVAL_DEFAULT = 1000;

export const retry = (fn, retriesLeft = RETRY_COUNT_DEFAULT, interval = RETRY_INTERVAL_DEFAULT) =>
  new Promise((resolve, reject) => {
    fn()
      .then(resolve)
      .catch(error => {
        setTimeout(() => {
          if (retriesLeft <= 1) {
            // maximum retries exceeded
            reject(error);
            return;
          }

          retry(fn, interval, retriesLeft - 1)
            .then(resolve)
            .catch(reject);
        }, interval);
      });
  });
