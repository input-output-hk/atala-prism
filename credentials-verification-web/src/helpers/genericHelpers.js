const firstLetterAsUpperCase = word => customUpperCase(word.charAt(0));

export const getInitials = (name = '') => {
  const words = name.split(' ');

  const initials = words.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);

    return `${accumulator}${upperCaseInitial}`;
  }, '');

  return initials;
};

export const getLogoAsBase64 = logo => `data:image/png;base64,${logo}`;

export const getLastArrayElementOrEmpty = array => (array.length ? array[array.length - 1] : {});

const modifyChar = (char, modifier) => (char.match(/[a-z]/i) ? modifier(char) : char);

const customCharByCharModifier = (string, modifier) => {
  let modifiedString = '';

  for (let i = 0; i < string.length; i++) {
    modifiedString += modifyChar(string.charAt(i), modifier);
  }

  return modifiedString;
};

const upperCase = string => string.toUpperCase();
const lowerCase = string => string.toLowerCase();

export const customUpperCase = string => customCharByCharModifier(string, upperCase);

export const customLowerCase = string => customCharByCharModifier(string, lowerCase);

export const mockDelay = timeoutMs => new Promise(resolve => setTimeout(resolve, timeoutMs));
