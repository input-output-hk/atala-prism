import { useEffect } from 'react';
import { navigate } from 'gatsby';

// This is a work around to allow the main app to be rendered client side and be the default route

const Main = () => {
  useEffect(() => {
    navigate('/app');
  }, []);

  return null;
};

export default Main;
