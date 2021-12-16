import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import PioneersFooter from '../components/pioneersFooter/PioneersFooter';

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<PioneersFooter />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<PioneersFooter />);

    expect(screen.getByText('Privacy')).toBeTruthy();
    expect(screen.getByText('Terms')).toBeTruthy();
    expect(screen.getByText('AtalaPrism.io')).toBeTruthy();
  });
});
