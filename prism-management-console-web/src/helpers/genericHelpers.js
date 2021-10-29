import fetch from 'isomorphic-fetch';
import { DEFAULT_PAGE_SIZE, TIMEOUT_MULTIPLIER_MS } from './constants';

const firstLetterAsUpperCase = word => customUpperCase(word.charAt(0));

export const getInitials = (name = '') => {
  const words = name.split(' ');

  return words.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);

    return `${accumulator}${upperCaseInitial}`;
  }, '');
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

export const getAditionalTimeout = entities =>
  entities > DEFAULT_PAGE_SIZE ? entities * TIMEOUT_MULTIPLIER_MS : 0;

export const svgPathToEncodedBase64 = async path => {
  const fetchedData = await fetch(path);
  const blob = await fetchedData.blob();
  const logoBlob = new File([blob], path, { type: 'image/svg+xml' });
  return blobToBase64(logoBlob);
};

export const blobToBase64 = file =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
