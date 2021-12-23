import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import DownloadButtons from '../components/landing/Molecules/DownloadButtons/DownloadButtons';

describe('<DownloadButtons />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<DownloadButtons />);

    expect(screen.getByAltText('Download iOS')).toBeTruthy();
    expect(screen.getByAltText('Download Android')).toBeTruthy();
  });
});
