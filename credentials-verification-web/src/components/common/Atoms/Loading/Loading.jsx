import React from 'react';
import { ClipLoader } from 'react-spinners';

const Loading = () => (
  <div style={{ height: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
    <ClipLoader loading size={150} />
  </div>
);
export default Loading;
