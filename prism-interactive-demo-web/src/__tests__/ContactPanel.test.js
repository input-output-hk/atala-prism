import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import ContactPanel from '../components/landing/Organisms/ContactPanel/ContactPanel';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<ContactPanel />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<ContactPanel />);

    expect(screen.getByText('landing.contactPanel.title')).toBeTruthy();
  });
});
