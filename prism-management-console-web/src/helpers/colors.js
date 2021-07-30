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

export const themeColors = [
  '#D8D8D8',
  '#FF2D3B',
  '#40EAEA',
  '#2BB5B5',
  '#2ACA9A',
  '#0C8762',
  '#EC2691',
  '#FFD100',
  '#FF8718',
  '#AF2DFF',
  '#4A2DFF',
  '#011727',
  '#808080'
];

export const backgroundColors = ['#FFFFFF', '#0C8762', '#2ACA9A', '#79CEB5', '#D8D8D8', '#000000'];

export const getContrastColor = backgroundColor => {
  const black = '#000000';
  const white = '#FFFFFF';

  const backgroundVsBlackRatio = contrast.ratio(black, backgroundColor);
  const backgroundVsWhiteRatio = contrast.ratio(white, backgroundColor);

  return backgroundVsBlackRatio > backgroundVsWhiteRatio ? black : white;
};
