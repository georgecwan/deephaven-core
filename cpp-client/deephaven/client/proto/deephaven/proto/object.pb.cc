// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: deephaven/proto/object.proto

#include "deephaven/proto/object.pb.h"

#include <algorithm>

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/wire_format_lite.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>

PROTOBUF_PRAGMA_INIT_SEG
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {
constexpr FetchObjectRequest::FetchObjectRequest(
  ::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized)
  : source_id_(nullptr){}
struct FetchObjectRequestDefaultTypeInternal {
  constexpr FetchObjectRequestDefaultTypeInternal()
    : _instance(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized{}) {}
  ~FetchObjectRequestDefaultTypeInternal() {}
  union {
    FetchObjectRequest _instance;
  };
};
PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT FetchObjectRequestDefaultTypeInternal _FetchObjectRequest_default_instance_;
constexpr FetchObjectResponse::FetchObjectResponse(
  ::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized)
  : typed_export_id_()
  , type_(&::PROTOBUF_NAMESPACE_ID::internal::fixed_address_empty_string)
  , data_(&::PROTOBUF_NAMESPACE_ID::internal::fixed_address_empty_string){}
struct FetchObjectResponseDefaultTypeInternal {
  constexpr FetchObjectResponseDefaultTypeInternal()
    : _instance(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized{}) {}
  ~FetchObjectResponseDefaultTypeInternal() {}
  union {
    FetchObjectResponse _instance;
  };
};
PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT FetchObjectResponseDefaultTypeInternal _FetchObjectResponse_default_instance_;
}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io
static ::PROTOBUF_NAMESPACE_ID::Metadata file_level_metadata_deephaven_2fproto_2fobject_2eproto[2];
static constexpr ::PROTOBUF_NAMESPACE_ID::EnumDescriptor const** file_level_enum_descriptors_deephaven_2fproto_2fobject_2eproto = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::ServiceDescriptor const** file_level_service_descriptors_deephaven_2fproto_2fobject_2eproto = nullptr;

const ::PROTOBUF_NAMESPACE_ID::uint32 TableStruct_deephaven_2fproto_2fobject_2eproto::offsets[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  ~0u,  // no _has_bits_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectRequest, _internal_metadata_),
  ~0u,  // no _extensions_
  ~0u,  // no _oneof_case_
  ~0u,  // no _weak_field_map_
  ~0u,  // no _inlined_string_donated_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectRequest, source_id_),
  ~0u,  // no _has_bits_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectResponse, _internal_metadata_),
  ~0u,  // no _extensions_
  ~0u,  // no _oneof_case_
  ~0u,  // no _weak_field_map_
  ~0u,  // no _inlined_string_donated_
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectResponse, type_),
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectResponse, data_),
  PROTOBUF_FIELD_OFFSET(::io::deephaven::proto::backplane::grpc::FetchObjectResponse, typed_export_id_),
};
static const ::PROTOBUF_NAMESPACE_ID::internal::MigrationSchema schemas[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  { 0, -1, -1, sizeof(::io::deephaven::proto::backplane::grpc::FetchObjectRequest)},
  { 7, -1, -1, sizeof(::io::deephaven::proto::backplane::grpc::FetchObjectResponse)},
};

static ::PROTOBUF_NAMESPACE_ID::Message const * const file_default_instances[] = {
  reinterpret_cast<const ::PROTOBUF_NAMESPACE_ID::Message*>(&::io::deephaven::proto::backplane::grpc::_FetchObjectRequest_default_instance_),
  reinterpret_cast<const ::PROTOBUF_NAMESPACE_ID::Message*>(&::io::deephaven::proto::backplane::grpc::_FetchObjectResponse_default_instance_),
};

const char descriptor_table_protodef_deephaven_2fproto_2fobject_2eproto[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) =
  "\n\034deephaven/proto/object.proto\022!io.deeph"
  "aven.proto.backplane.grpc\032\034deephaven/pro"
  "to/ticket.proto\"W\n\022FetchObjectRequest\022A\n"
  "\tsource_id\030\001 \001(\0132..io.deephaven.proto.ba"
  "ckplane.grpc.TypedTicket\"z\n\023FetchObjectR"
  "esponse\022\014\n\004type\030\001 \001(\t\022\014\n\004data\030\002 \001(\014\022G\n\017t"
  "yped_export_id\030\003 \003(\0132..io.deephaven.prot"
  "o.backplane.grpc.TypedTicket2\217\001\n\rObjectS"
  "ervice\022~\n\013FetchObject\0225.io.deephaven.pro"
  "to.backplane.grpc.FetchObjectRequest\0326.i"
  "o.deephaven.proto.backplane.grpc.FetchOb"
  "jectResponse\"\000B\004H\001P\001b\006proto3"
  ;
static const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable*const descriptor_table_deephaven_2fproto_2fobject_2eproto_deps[1] = {
  &::descriptor_table_deephaven_2fproto_2fticket_2eproto,
};
static ::PROTOBUF_NAMESPACE_ID::internal::once_flag descriptor_table_deephaven_2fproto_2fobject_2eproto_once;
const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_deephaven_2fproto_2fobject_2eproto = {
  false, false, 468, descriptor_table_protodef_deephaven_2fproto_2fobject_2eproto, "deephaven/proto/object.proto", 
  &descriptor_table_deephaven_2fproto_2fobject_2eproto_once, descriptor_table_deephaven_2fproto_2fobject_2eproto_deps, 1, 2,
  schemas, file_default_instances, TableStruct_deephaven_2fproto_2fobject_2eproto::offsets,
  file_level_metadata_deephaven_2fproto_2fobject_2eproto, file_level_enum_descriptors_deephaven_2fproto_2fobject_2eproto, file_level_service_descriptors_deephaven_2fproto_2fobject_2eproto,
};
PROTOBUF_ATTRIBUTE_WEAK const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable* descriptor_table_deephaven_2fproto_2fobject_2eproto_getter() {
  return &descriptor_table_deephaven_2fproto_2fobject_2eproto;
}

// Force running AddDescriptors() at dynamic initialization time.
PROTOBUF_ATTRIBUTE_INIT_PRIORITY static ::PROTOBUF_NAMESPACE_ID::internal::AddDescriptorsRunner dynamic_init_dummy_deephaven_2fproto_2fobject_2eproto(&descriptor_table_deephaven_2fproto_2fobject_2eproto);
namespace io {
namespace deephaven {
namespace proto {
namespace backplane {
namespace grpc {

// ===================================================================

class FetchObjectRequest::_Internal {
 public:
  static const ::io::deephaven::proto::backplane::grpc::TypedTicket& source_id(const FetchObjectRequest* msg);
};

const ::io::deephaven::proto::backplane::grpc::TypedTicket&
FetchObjectRequest::_Internal::source_id(const FetchObjectRequest* msg) {
  return *msg->source_id_;
}
void FetchObjectRequest::clear_source_id() {
  if (GetArenaForAllocation() == nullptr && source_id_ != nullptr) {
    delete source_id_;
  }
  source_id_ = nullptr;
}
FetchObjectRequest::FetchObjectRequest(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                         bool is_message_owned)
  : ::PROTOBUF_NAMESPACE_ID::Message(arena, is_message_owned) {
  SharedCtor();
  if (!is_message_owned) {
    RegisterArenaDtor(arena);
  }
  // @@protoc_insertion_point(arena_constructor:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
}
FetchObjectRequest::FetchObjectRequest(const FetchObjectRequest& from)
  : ::PROTOBUF_NAMESPACE_ID::Message() {
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  if (from._internal_has_source_id()) {
    source_id_ = new ::io::deephaven::proto::backplane::grpc::TypedTicket(*from.source_id_);
  } else {
    source_id_ = nullptr;
  }
  // @@protoc_insertion_point(copy_constructor:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
}

void FetchObjectRequest::SharedCtor() {
source_id_ = nullptr;
}

FetchObjectRequest::~FetchObjectRequest() {
  // @@protoc_insertion_point(destructor:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  if (GetArenaForAllocation() != nullptr) return;
  SharedDtor();
  _internal_metadata_.Delete<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

inline void FetchObjectRequest::SharedDtor() {
  GOOGLE_DCHECK(GetArenaForAllocation() == nullptr);
  if (this != internal_default_instance()) delete source_id_;
}

void FetchObjectRequest::ArenaDtor(void* object) {
  FetchObjectRequest* _this = reinterpret_cast< FetchObjectRequest* >(object);
  (void)_this;
}
void FetchObjectRequest::RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena*) {
}
void FetchObjectRequest::SetCachedSize(int size) const {
  _cached_size_.Set(size);
}

void FetchObjectRequest::Clear() {
// @@protoc_insertion_point(message_clear_start:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  if (GetArenaForAllocation() == nullptr && source_id_ != nullptr) {
    delete source_id_;
  }
  source_id_ = nullptr;
  _internal_metadata_.Clear<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

const char* FetchObjectRequest::_InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) {
#define CHK_(x) if (PROTOBUF_PREDICT_FALSE(!(x))) goto failure
  while (!ctx->Done(&ptr)) {
    ::PROTOBUF_NAMESPACE_ID::uint32 tag;
    ptr = ::PROTOBUF_NAMESPACE_ID::internal::ReadTag(ptr, &tag);
    switch (tag >> 3) {
      // .io.deephaven.proto.backplane.grpc.TypedTicket source_id = 1;
      case 1:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 10)) {
          ptr = ctx->ParseMessage(_internal_mutable_source_id(), ptr);
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      default:
        goto handle_unusual;
    }  // switch
  handle_unusual:
    if ((tag == 0) || ((tag & 7) == 4)) {
      CHK_(ptr);
      ctx->SetLastTag(tag);
      goto message_done;
    }
    ptr = UnknownFieldParse(
        tag,
        _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(),
        ptr, ctx);
    CHK_(ptr != nullptr);
  }  // while
message_done:
  return ptr;
failure:
  ptr = nullptr;
  goto message_done;
#undef CHK_
}

::PROTOBUF_NAMESPACE_ID::uint8* FetchObjectRequest::_InternalSerialize(
    ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const {
  // @@protoc_insertion_point(serialize_to_array_start:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  // .io.deephaven.proto.backplane.grpc.TypedTicket source_id = 1;
  if (this->_internal_has_source_id()) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::
      InternalWriteMessage(
        1, _Internal::source_id(this), target, stream);
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormat::InternalSerializeUnknownFieldsToArray(
        _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance), target, stream);
  }
  // @@protoc_insertion_point(serialize_to_array_end:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  return target;
}

size_t FetchObjectRequest::ByteSizeLong() const {
// @@protoc_insertion_point(message_byte_size_start:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  size_t total_size = 0;

  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  // .io.deephaven.proto.backplane.grpc.TypedTicket source_id = 1;
  if (this->_internal_has_source_id()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::MessageSize(
        *source_id_);
  }

  return MaybeComputeUnknownFieldsSize(total_size, &_cached_size_);
}

const ::PROTOBUF_NAMESPACE_ID::Message::ClassData FetchObjectRequest::_class_data_ = {
    ::PROTOBUF_NAMESPACE_ID::Message::CopyWithSizeCheck,
    FetchObjectRequest::MergeImpl
};
const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*FetchObjectRequest::GetClassData() const { return &_class_data_; }

void FetchObjectRequest::MergeImpl(::PROTOBUF_NAMESPACE_ID::Message* to,
                      const ::PROTOBUF_NAMESPACE_ID::Message& from) {
  static_cast<FetchObjectRequest *>(to)->MergeFrom(
      static_cast<const FetchObjectRequest &>(from));
}


void FetchObjectRequest::MergeFrom(const FetchObjectRequest& from) {
// @@protoc_insertion_point(class_specific_merge_from_start:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  GOOGLE_DCHECK_NE(&from, this);
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  if (from._internal_has_source_id()) {
    _internal_mutable_source_id()->::io::deephaven::proto::backplane::grpc::TypedTicket::MergeFrom(from._internal_source_id());
  }
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
}

void FetchObjectRequest::CopyFrom(const FetchObjectRequest& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:io.deephaven.proto.backplane.grpc.FetchObjectRequest)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool FetchObjectRequest::IsInitialized() const {
  return true;
}

void FetchObjectRequest::InternalSwap(FetchObjectRequest* other) {
  using std::swap;
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  swap(source_id_, other->source_id_);
}

::PROTOBUF_NAMESPACE_ID::Metadata FetchObjectRequest::GetMetadata() const {
  return ::PROTOBUF_NAMESPACE_ID::internal::AssignDescriptors(
      &descriptor_table_deephaven_2fproto_2fobject_2eproto_getter, &descriptor_table_deephaven_2fproto_2fobject_2eproto_once,
      file_level_metadata_deephaven_2fproto_2fobject_2eproto[0]);
}

// ===================================================================

class FetchObjectResponse::_Internal {
 public:
};

void FetchObjectResponse::clear_typed_export_id() {
  typed_export_id_.Clear();
}
FetchObjectResponse::FetchObjectResponse(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                         bool is_message_owned)
  : ::PROTOBUF_NAMESPACE_ID::Message(arena, is_message_owned),
  typed_export_id_(arena) {
  SharedCtor();
  if (!is_message_owned) {
    RegisterArenaDtor(arena);
  }
  // @@protoc_insertion_point(arena_constructor:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
}
FetchObjectResponse::FetchObjectResponse(const FetchObjectResponse& from)
  : ::PROTOBUF_NAMESPACE_ID::Message(),
      typed_export_id_(from.typed_export_id_) {
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  type_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_type().empty()) {
    type_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, from._internal_type(), 
      GetArenaForAllocation());
  }
  data_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_data().empty()) {
    data_.Set(::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::EmptyDefault{}, from._internal_data(), 
      GetArenaForAllocation());
  }
  // @@protoc_insertion_point(copy_constructor:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
}

void FetchObjectResponse::SharedCtor() {
type_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
data_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}

FetchObjectResponse::~FetchObjectResponse() {
  // @@protoc_insertion_point(destructor:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  if (GetArenaForAllocation() != nullptr) return;
  SharedDtor();
  _internal_metadata_.Delete<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

inline void FetchObjectResponse::SharedDtor() {
  GOOGLE_DCHECK(GetArenaForAllocation() == nullptr);
  type_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  data_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}

void FetchObjectResponse::ArenaDtor(void* object) {
  FetchObjectResponse* _this = reinterpret_cast< FetchObjectResponse* >(object);
  (void)_this;
}
void FetchObjectResponse::RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena*) {
}
void FetchObjectResponse::SetCachedSize(int size) const {
  _cached_size_.Set(size);
}

void FetchObjectResponse::Clear() {
// @@protoc_insertion_point(message_clear_start:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  typed_export_id_.Clear();
  type_.ClearToEmpty();
  data_.ClearToEmpty();
  _internal_metadata_.Clear<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

const char* FetchObjectResponse::_InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) {
#define CHK_(x) if (PROTOBUF_PREDICT_FALSE(!(x))) goto failure
  while (!ctx->Done(&ptr)) {
    ::PROTOBUF_NAMESPACE_ID::uint32 tag;
    ptr = ::PROTOBUF_NAMESPACE_ID::internal::ReadTag(ptr, &tag);
    switch (tag >> 3) {
      // string type = 1;
      case 1:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 10)) {
          auto str = _internal_mutable_type();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(::PROTOBUF_NAMESPACE_ID::internal::VerifyUTF8(str, "io.deephaven.proto.backplane.grpc.FetchObjectResponse.type"));
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      // bytes data = 2;
      case 2:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 18)) {
          auto str = _internal_mutable_data();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      // repeated .io.deephaven.proto.backplane.grpc.TypedTicket typed_export_id = 3;
      case 3:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 26)) {
          ptr -= 1;
          do {
            ptr += 1;
            ptr = ctx->ParseMessage(_internal_add_typed_export_id(), ptr);
            CHK_(ptr);
            if (!ctx->DataAvailable(ptr)) break;
          } while (::PROTOBUF_NAMESPACE_ID::internal::ExpectTag<26>(ptr));
        } else
          goto handle_unusual;
        continue;
      default:
        goto handle_unusual;
    }  // switch
  handle_unusual:
    if ((tag == 0) || ((tag & 7) == 4)) {
      CHK_(ptr);
      ctx->SetLastTag(tag);
      goto message_done;
    }
    ptr = UnknownFieldParse(
        tag,
        _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(),
        ptr, ctx);
    CHK_(ptr != nullptr);
  }  // while
message_done:
  return ptr;
failure:
  ptr = nullptr;
  goto message_done;
#undef CHK_
}

::PROTOBUF_NAMESPACE_ID::uint8* FetchObjectResponse::_InternalSerialize(
    ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const {
  // @@protoc_insertion_point(serialize_to_array_start:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  // string type = 1;
  if (!this->_internal_type().empty()) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_type().data(), static_cast<int>(this->_internal_type().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "io.deephaven.proto.backplane.grpc.FetchObjectResponse.type");
    target = stream->WriteStringMaybeAliased(
        1, this->_internal_type(), target);
  }

  // bytes data = 2;
  if (!this->_internal_data().empty()) {
    target = stream->WriteBytesMaybeAliased(
        2, this->_internal_data(), target);
  }

  // repeated .io.deephaven.proto.backplane.grpc.TypedTicket typed_export_id = 3;
  for (unsigned int i = 0,
      n = static_cast<unsigned int>(this->_internal_typed_export_id_size()); i < n; i++) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::
      InternalWriteMessage(3, this->_internal_typed_export_id(i), target, stream);
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormat::InternalSerializeUnknownFieldsToArray(
        _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance), target, stream);
  }
  // @@protoc_insertion_point(serialize_to_array_end:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  return target;
}

size_t FetchObjectResponse::ByteSizeLong() const {
// @@protoc_insertion_point(message_byte_size_start:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  size_t total_size = 0;

  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  // repeated .io.deephaven.proto.backplane.grpc.TypedTicket typed_export_id = 3;
  total_size += 1UL * this->_internal_typed_export_id_size();
  for (const auto& msg : this->typed_export_id_) {
    total_size +=
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::MessageSize(msg);
  }

  // string type = 1;
  if (!this->_internal_type().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_type());
  }

  // bytes data = 2;
  if (!this->_internal_data().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::BytesSize(
        this->_internal_data());
  }

  return MaybeComputeUnknownFieldsSize(total_size, &_cached_size_);
}

const ::PROTOBUF_NAMESPACE_ID::Message::ClassData FetchObjectResponse::_class_data_ = {
    ::PROTOBUF_NAMESPACE_ID::Message::CopyWithSizeCheck,
    FetchObjectResponse::MergeImpl
};
const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*FetchObjectResponse::GetClassData() const { return &_class_data_; }

void FetchObjectResponse::MergeImpl(::PROTOBUF_NAMESPACE_ID::Message* to,
                      const ::PROTOBUF_NAMESPACE_ID::Message& from) {
  static_cast<FetchObjectResponse *>(to)->MergeFrom(
      static_cast<const FetchObjectResponse &>(from));
}


void FetchObjectResponse::MergeFrom(const FetchObjectResponse& from) {
// @@protoc_insertion_point(class_specific_merge_from_start:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  GOOGLE_DCHECK_NE(&from, this);
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  typed_export_id_.MergeFrom(from.typed_export_id_);
  if (!from._internal_type().empty()) {
    _internal_set_type(from._internal_type());
  }
  if (!from._internal_data().empty()) {
    _internal_set_data(from._internal_data());
  }
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
}

void FetchObjectResponse::CopyFrom(const FetchObjectResponse& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:io.deephaven.proto.backplane.grpc.FetchObjectResponse)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool FetchObjectResponse::IsInitialized() const {
  return true;
}

void FetchObjectResponse::InternalSwap(FetchObjectResponse* other) {
  using std::swap;
  auto* lhs_arena = GetArenaForAllocation();
  auto* rhs_arena = other->GetArenaForAllocation();
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  typed_export_id_.InternalSwap(&other->typed_export_id_);
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      &type_, lhs_arena,
      &other->type_, rhs_arena
  );
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      &data_, lhs_arena,
      &other->data_, rhs_arena
  );
}

::PROTOBUF_NAMESPACE_ID::Metadata FetchObjectResponse::GetMetadata() const {
  return ::PROTOBUF_NAMESPACE_ID::internal::AssignDescriptors(
      &descriptor_table_deephaven_2fproto_2fobject_2eproto_getter, &descriptor_table_deephaven_2fproto_2fobject_2eproto_once,
      file_level_metadata_deephaven_2fproto_2fobject_2eproto[1]);
}

// @@protoc_insertion_point(namespace_scope)
}  // namespace grpc
}  // namespace backplane
}  // namespace proto
}  // namespace deephaven
}  // namespace io
PROTOBUF_NAMESPACE_OPEN
template<> PROTOBUF_NOINLINE ::io::deephaven::proto::backplane::grpc::FetchObjectRequest* Arena::CreateMaybeMessage< ::io::deephaven::proto::backplane::grpc::FetchObjectRequest >(Arena* arena) {
  return Arena::CreateMessageInternal< ::io::deephaven::proto::backplane::grpc::FetchObjectRequest >(arena);
}
template<> PROTOBUF_NOINLINE ::io::deephaven::proto::backplane::grpc::FetchObjectResponse* Arena::CreateMaybeMessage< ::io::deephaven::proto::backplane::grpc::FetchObjectResponse >(Arena* arena) {
  return Arena::CreateMessageInternal< ::io::deephaven::proto::backplane::grpc::FetchObjectResponse >(arena);
}
PROTOBUF_NAMESPACE_CLOSE

// @@protoc_insertion_point(global_scope)
#include <google/protobuf/port_undef.inc>