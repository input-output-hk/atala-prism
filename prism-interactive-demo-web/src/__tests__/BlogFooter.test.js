import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import BlogFooter from '../components/footer/BlogFooter';

describe('<BlogFooter />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<BlogFooter />);

    expect(screen.getByAltText('footer')).toBeTruthy();
    expect(screen.getByAltText('Atala-Logo')).toBeTruthy();
    expect(screen.getByText('Terms and Conditions')).toBeTruthy();
    expect(screen.getByText('Privacy Policy')).toBeTruthy();
    expect(screen.getByText('Copyright © 2021 IOHK')).toBeTruthy();
  });
});
