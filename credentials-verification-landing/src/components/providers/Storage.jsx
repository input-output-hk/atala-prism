export const setStoredItem = (key, value) => {
  localStorage.setItem(key, value);
};

export const getStoredItem = key => {
  localStorage.getItem(key);
};

export const removeStoredItem = key => {
  localStorage.removeItem(key);
};
