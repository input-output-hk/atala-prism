import { LOGO } from '../../helpers/constants';

const firstLetterAsUpperCase = word => word.charAt(0).toUpperCase();

export const getInitials = name => {
  const words = name.split(' ');

  return words.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);

    return `${accumulator}${upperCaseInitial}`;
  }, '');
};

export const getLogoAsBase64 = () => `data:image/png;base64,${localStorage.getItem(LOGO)}`;

export const scrollToRef = ref => window.scrollTo(0, ref.current.offsetTop);

export const scrollToTop = () => window.scrollTo(0, 0);
