import React from 'react';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';

import './_style.scss';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

const translationPrefix = 'credentials.student';

const RenderStudent = ({ imageSrc, imageAlt, name }) => {
  const tp = useTranslationWithPrefix(translationPrefix);

  return (
    <div className="StudentAvatar">
      {imageSrc && (
        <img style={{ height: '50px', width: '50px' }} src={imageSrc} alt={imageAlt || tp('alt')} />
      )}
      <CellRenderer value={name} title={tp('table.columns.student')} />
    </div>
  );
};

RenderStudent.defaultProps = {
  imageSrc: '',
  imageAlt: ''
};

RenderStudent.propTypes = {
  imageSrc: PropTypes.string,
  imageAlt: PropTypes.string,
  name: PropTypes.string.isRequired
};

export default RenderStudent;
