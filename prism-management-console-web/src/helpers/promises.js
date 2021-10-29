const promiseWasCancelled = 'Promise was cancelled';

export class PromiseCancellationError extends Error {
  constructor(message) {
    super();
    this.message = message;
    this.name = promiseWasCancelled;
  }
}
const promiseCancellationError = new PromiseCancellationError();

export const makeCancelable = promise => {
  let hasCanceled_ = false;
  const wrappedPromise = new Promise((resolve, reject) => {
    promise
      .then(val => (hasCanceled_ ? reject(promiseCancellationError) : resolve(val)))
      .catch(err => {
        const error = new PromiseCancellationError();
        error.message = err.message;
        error.stack = err.stack;
        return reject(error);
      });
  });

  return {
    promise: wrappedPromise,
    cancel() {
      hasCanceled_ = true;
    }
  };
};

export const retry = (fn, retriesLeft = 5, interval = 1000) =>
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
