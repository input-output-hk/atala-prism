import React from 'react';
import { cleanup, render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Landing from '../components/landing/Landing';
import {
  BENEFITS_NAME,
  CASE_STUDY_NAME,
  COMPONENTS_NAME,
  CONTACT_US_NAME,
  DEMO_NAME,
  FAQ_NAME,
  GET_STARTED_NAME,
  USE_CASES_NAME,
  VISION_NAME
} from '../helpers/constants';

const keys = [
  { key: VISION_NAME, title: 'landing.intro.question' },
  { key: DEMO_NAME, title: 'landing.findCredential.title' },
  { key: COMPONENTS_NAME, title: 'landing.credential.title' },
  { key: BENEFITS_NAME, title: 'landing.trust.predownloadText' },
  { key: USE_CASES_NAME, title: 'landing.useCasesPanel.title' },
  { key: CASE_STUDY_NAME, title: 'landing.caseStudy.title' },
  { key: GET_STARTED_NAME, title: 'landing.getStarted.title' },
  { key: FAQ_NAME, title: 'landing.faqPanel.title' },
  { key: CONTACT_US_NAME, title: 'landing.contactPanel.title' },
  { key: null, title: 'landing.start.info' }
];

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

jest.mock('gatsby-plugin-firebase', () => {
  const analytics = () => ({
    logEvent: () => {}
  });

  return { analytics };
});

const mockHeader = jest.fn();
jest.mock('../components/Header/Header', () => props => {
  mockHeader(props);
  return <mock-Header />;
});

describe('<Landing />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<Landing isTesting />);

    expect(screen.getByText('landing.start.info')).toBeTruthy();
    expect(screen.getByText('landing.start.subtitle')).toBeTruthy();
    expect(screen.getByAltText('pioneer')).toBeTruthy();
    expect(screen.getByText('landing.start.join')).toBeTruthy();
    expect(screen.getByText('landing.start.earlyAccess')).toBeTruthy();
    expect(screen.getByText('actions.register')).toBeTruthy();
  });

  it('mouse over event is triggered for each section', () => {
    render(<Landing isTesting />);

    expect(mockHeader).toHaveBeenCalledWith({ currentSection: null });

    keys.forEach(({ key, title }) => {
      userEvent.hover(screen.getByText(title));
      expect(mockHeader).toHaveBeenCalledWith({ currentSection: key });
    });
  });

  it('focus event is triggered for each section', () => {
    render(<Landing isTesting />);

    expect(mockHeader).toHaveBeenCalledWith({ currentSection: null });

    keys.forEach(({ key, title }) => {
      fireEvent.focus(screen.getByText(title));
      expect(mockHeader).toHaveBeenCalledWith({ currentSection: key });
    });
  });
});
