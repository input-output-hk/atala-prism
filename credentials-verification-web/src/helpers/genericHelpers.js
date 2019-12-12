const firstLetterAsUpperCase = word => word.charAt(0).toUpperCase();

export const getInitials = name => {
  const words = name.split(' ');

  const initials = words.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);

    return `${accumulator}${upperCaseInitial}`;
  }, '');

  return initials;
};
