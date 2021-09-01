import React, { useEffect } from 'react';
import { navigate } from 'gatsby';
import SEO from '../components/seo/seo';

// This is a work around to allow the main app to be rendered client side and be the default route

const Main = () => {
  useEffect(() => {
    navigate('/app');
  }, []);

  return (
    <div>
      <SEO />
    </div>
  );
};

export default Main;
