import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import NavBar from '../components/NavBar/NavBar';
import {
  BENEFITS_NAME,
  CASE_STUDY_NAME,
  COMPONENTS_NAME,
  DEMO_NAME,
  FAQ_NAME,
  GET_STARTED_NAME,
  USE_CASES_NAME,
  VISION_NAME
} from '../helpers/constants';

const keys = [
  VISION_NAME,
  DEMO_NAME,
  COMPONENTS_NAME,
  BENEFITS_NAME,
  USE_CASES_NAME,
  CASE_STUDY_NAME,
  GET_STARTED_NAME,
  FAQ_NAME
];

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<NavBar />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<NavBar currentSection={VISION_NAME} />);

    keys.forEach(key => {
      expect(screen.getByText(`navBar.menuItems.${key}`)).toBeTruthy();
    });
  });
});
