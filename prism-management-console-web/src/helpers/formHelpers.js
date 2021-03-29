export const getFirstError = error => error?.errorFields[0]?.errors[0] || error.message || error;
