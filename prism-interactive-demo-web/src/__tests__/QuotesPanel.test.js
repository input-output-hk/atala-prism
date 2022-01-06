import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import QuotesPanel from '../components/landing/Molecules/QuotesPanel/QuotesPanel';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<QuotesPanel />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<QuotesPanel />);

    expect(screen.getByText('landing.quotes.phrase')).toBeTruthy();
  });
});
