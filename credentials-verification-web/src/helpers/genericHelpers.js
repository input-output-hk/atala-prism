import { LOGO } from './constants';

const firstLetterAsUpperCase = word => customUpperCase(word.charAt(0));

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

const charToUpperCase = char => (char.match(/[a-z]/i) ? char.toUpperCase() : char);

export const customUpperCase = string => {
  let upperCasedString = '';

  for (let i = 0; i < string.length; i++) {
    upperCasedString += charToUpperCase(string.charAt(i));
  }

  return upperCasedString;
};
