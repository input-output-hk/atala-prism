import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { VISION_NAME } from '../helpers/constants';
import Header from '../components/Header/Header';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<Header />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<Header currentSection={VISION_NAME} />);

    expect(screen.getByAltText('atalaLogo')).toBeTruthy();
  });
});
