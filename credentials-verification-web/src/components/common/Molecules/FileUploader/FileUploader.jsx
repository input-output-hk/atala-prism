import React, { useState } from 'react';
import { Icon, message, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';

import './_style.scss';

const FileUploader = ({ hint, field, accept, savePicture, uploadText, formRef, initialValue }) => {
  const { t } = useTranslation();
  const [fileList, setFileList] = useState(initialValue ? [initialValue] : []);

  const onChange = ({ fileList: newFileList }) => !newFileList.length && setFileList([]);

  const customRequest = ({ file, onError, onSuccess }) => {
    savePicture(file)
      .then(response => {
        formRef.current.getForm().setFieldsValue({
          [field]: [file]
        });
        setFileList([file]);
        onSuccess(response);
      })
      .catch(({ error: errorMessage }) => {
        message.error(t(errorMessage));
        onError();
      });
  };

  const uploadProps = Object.assign(
    {},
    { accept },
    {
      fileList,
      onChange,
      customRequest
    }
  );

  return (
    <div className="FileUploader">
      <p>{hint && t(hint)}</p>
      <Upload {...uploadProps}>
        {fileList.length ? (
          <div />
        ) : (
          <CustomButton
            buttonProps={{ className: 'theme-outline' }}
            buttonText={t(uploadText)}
            icon={<Icon type="upload" />}
          />
        )}
      </Upload>
    </div>
  );
};

FileUploader.defaultProps = {
  hint: '',
  accept: undefined
};

FileUploader.propTypes = {
  hint: PropTypes.string,
  field: PropTypes.string.isRequired,
  accept: PropTypes.string,
  savePicture: PropTypes.func.isRequired,
  uploadText: PropTypes.string.isRequired,
  formRef: PropTypes.oneOfType([PropTypes.func, PropTypes.shape({ current: PropTypes.object })])
    .isRequired
};

export default FileUploader;
