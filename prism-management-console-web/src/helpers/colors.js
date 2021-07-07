import contrast from 'get-contrast';

// this constant was created for use color variables in .js files,
// match these colors with those defined in general_styling
export const colors = {
  primary: '#F83633',
  primaryDarker: '#BA2926',
  secondary: '#011727',
  secondaryDisabled: '#0000004d',
  success: '#1ed69e',
  error: '#F83633'
};

export const getContrastColor = backgroundColor => {
  const black = '#000000';
  const white = '#FFFFFF';

  const backgroundVsBlackRatio = contrast.ratio(black, backgroundColor);
  const backgroundVsWhiteRatio = contrast.ratio(white, backgroundColor);

  return backgroundVsBlackRatio > backgroundVsWhiteRatio ? black : white;
};
