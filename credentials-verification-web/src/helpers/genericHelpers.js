import { LOGO } from './constants';

const firstLetterAsUpperCase = word => word.charAt(0).toUpperCase();

export const getInitials = name => {
  const words = name.split(' ');

  const initials = words.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);

    return `${accumulator}${upperCaseInitial}`;
  }, '');

  return initials;
};

export const getLogoAsBase64 = () => `data:image/png;base64,${localStorage.getItem(LOGO)}`;

export const getLastArrayElementOrEmpty = array => (array.length ? array[array.length - 1] : {});
