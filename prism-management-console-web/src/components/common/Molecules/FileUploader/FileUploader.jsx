import React, { useState } from 'react';
import { UploadOutlined } from '@ant-design/icons';
import { message, Upload } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import { refPropShape } from '../../../../helpers/propShapes';

import './_style.scss';

const FileUploader = ({
  hint,
  field,
  accept,
  savePicture,
  uploadText,
  formRef,
  initialValue,
  disabled,
  updateFile
}) => {
  const { t } = useTranslation();

  const [fileList, setFileList] = useState(initialValue ? [initialValue] : []);

  const onChange = ({ fileList: newFileList }) => !newFileList.length && setFileList([]);

  const customRequest = ({ file, onError, onSuccess }) => {
    savePicture(file)
      .then(response => {
        formRef.current.getForm().setFieldsValue({
          [field]: [response]
        });
        if (updateFile) updateFile(file);
        setFileList([file]);
        onSuccess(response);
      })
      .catch(({ message: errorMessage }) => {
        message.error(t(`errors.saveFile.${errorMessage}`));
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
            buttonProps={{ className: 'theme-outline', disabled, icon: <UploadOutlined /> }}
            buttonText={t(uploadText)}
          />
        )}
      </Upload>
    </div>
  );
};

FileUploader.defaultProps = {
  hint: '',
  accept: undefined,
  initialValue: null,
  disabled: false
};

FileUploader.defaultProps = {
  updateFile: undefined
};

FileUploader.propTypes = {
  hint: PropTypes.string,
  field: PropTypes.string.isRequired,
  accept: PropTypes.string,
  savePicture: PropTypes.func.isRequired,
  uploadText: PropTypes.string.isRequired,
  formRef: refPropShape.isRequired,
  initialValue: PropTypes.arrayOf(PropTypes.instanceOf(ArrayBuffer)),
  disabled: PropTypes.bool,
  updateFile: PropTypes.func
};

export default FileUploader;
