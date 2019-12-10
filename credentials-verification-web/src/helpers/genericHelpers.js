const firstLetterAsUpperCase = word => word.charAt(0).toUpperCase();

export const getInitials = name => {
  const [firstWord, ...rest] = name.split(' ');

  const firstInitial = firstLetterAsUpperCase(firstWord);

  if (!rest.length) {
    return firstInitial;
  }

  const initials = rest.reduce((accumulator, nextWord) => {
    const upperCaseInitial = firstLetterAsUpperCase(nextWord);
    return `${accumulator}${upperCaseInitial}`;
  }, firstInitial);

  return initials;
};
