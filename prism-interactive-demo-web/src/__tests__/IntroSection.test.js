import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import IntroSection from '../components/landing/Organisms/IntroSection/IntroSection';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<IntroSection />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<IntroSection />);

    expect(screen.getByText('landing.intro.itemIcon.credentials.title')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.wallet.title')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.crypto.title')).toBeTruthy();
  });
});
