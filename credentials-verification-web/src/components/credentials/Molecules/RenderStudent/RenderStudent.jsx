import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';

import './_style.scss';

const RenderStudent = ({ imageSrc, imageAlt, name }) => {
  const { t } = useTranslation();

  const title = 'student';

  return (
    <div className="StudentAvatar">
      {imageSrc && (
        <img
          style={{ height: '50px', width: '50px' }}
          src={imageSrc}
          alt={imageAlt || t('credential.student.alt')}
        />
      )}
      <CellRenderer value={name} title={title} componentName="credentials.student" />
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
