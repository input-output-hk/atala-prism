import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FindCredential from '../components/landing/Molecules/FindCredential/FindCredential';
import { redirector } from '../app/components/providers/withRedirector';

jest.mock('../app/components/providers/withRedirector', () => {
  const mockRedirector = {
    redirectToCredentials: jest.fn()
  };
  return {
    redirector: mockRedirector,
    withRedirector: Component => props => <Component {...props} redirector={mockRedirector} />
  };
});

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

jest.mock('gatsby-plugin-firebase', () => {
  const analytics = () => ({
    logEvent: () => {}
  });

  return { analytics };
});

describe('<FindCredential />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<FindCredential isTesting />);

    expect(screen.getByText('landing.findCredential.title')).toBeTruthy();

    userEvent.click(screen.getByText('landing.findCredential.askForCredential'));

    expect(redirector.redirectToCredentials.mock.calls.length).toBe(1);
  });
});
